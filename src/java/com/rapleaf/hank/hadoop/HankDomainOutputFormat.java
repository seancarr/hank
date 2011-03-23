/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.rapleaf.hank.hadoop;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.FileAlreadyExistsException;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

import com.rapleaf.hank.storage.StorageEngine;
import com.rapleaf.hank.storage.StorageEngineFactory;
import com.rapleaf.hank.storage.Writer;

public class HankDomainOutputFormat implements OutputFormat<IntWritable, HankRecordWritable> {

  public static final String CONF_PARAMETER_OUTPUT_PATH = "hank.output.path";
  public static final String CONF_PARAMETER_STORAGE_ENGINE = "hank.storage.class";

  @Override
  public void checkOutputSpecs(FileSystem fs, JobConf conf)
  throws IOException {
    String outputPath = conf.get(CONF_PARAMETER_OUTPUT_PATH);
    if (fs.exists(new Path(outputPath))) {
      throw new FileAlreadyExistsException("Output root already exists: " + outputPath);
    }
  }

  @Override
  public RecordWriter<IntWritable, HankRecordWritable> getRecordWriter(
      FileSystem fs, JobConf conf, String name, Progressable progress) throws IOException {
    String outputPath = conf.get(CONF_PARAMETER_OUTPUT_PATH);
    StorageEngine storageEngine = null;
    try {
      storageEngine = buildStorageEngineFromConf(conf);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return new HankDomainRecordWriter(fs, storageEngine, outputPath);
  }

  StorageEngine buildStorageEngineFromConf(JobConf conf) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
    String storageEngineClassName = conf.get(CONF_PARAMETER_STORAGE_ENGINE);
    Class<StorageEngine> storageEngineClass = (Class<StorageEngine>) Class.forName(storageEngineClassName);
    Class[] storageEngineClassClasses = storageEngineClass.getDeclaredClasses();
    for (Class c : storageEngineClassClasses) {
      String storageEngineClassFactoryClassName = storageEngineClassName + "$Factory";
      if (c.getName().equals(storageEngineClassFactoryClassName)) {
        Class factoryClass = c;
        StorageEngineFactory factory = (StorageEngineFactory) factoryClass.newInstance();
        Class parameterTypes[] = {Map.class, String.class};;
        Method method = factoryClass.getMethod("getStorageEngine", parameterTypes);

        // TODO: load options and domainName from conf
        Map<String, Object> options = new HashMap<String, Object>();
        String domainName = "TestDomain";

        Object argList[] = {options, domainName};
        StorageEngine storageEngine = (StorageEngine) method.invoke(factory, argList);
        return storageEngine;
      }
    }
    throw new NoSuchFieldError("StorageEngine class " + storageEngineClassName + " should provide a Factory static class.");
  }

  private static class HankDomainRecordWriter implements RecordWriter<IntWritable, HankRecordWritable> {

    private static final String TMP_DIRECTORY_NAME = "_tmp_HankDomainRecordWriter";

    private FileSystem fs;
    private StorageEngine storageEngine;
    private String tmpOutputPath;
    private String finalOutputPath;
    private HadoopFSOutputStreamFactory tmpOutputStreamFactory;
    private Writer writer;
    private Integer writerPartition;
    private Set<Integer> writtenPartitions = new HashSet<Integer>();

    public HankDomainRecordWriter(FileSystem fs,
        StorageEngine storageEngine,
        String finalOutputPath) {
      this.fs = fs;
      this.storageEngine = storageEngine;
      this.tmpOutputPath = finalOutputPath + "/" + TMP_DIRECTORY_NAME + "/" + UUID.randomUUID().toString();
      this.finalOutputPath = finalOutputPath;
      this.tmpOutputStreamFactory = new HadoopFSOutputStreamFactory(fs, tmpOutputPath);
      this.writer = null;
      this.writerPartition = null;
    }

    @Override
    public void close(Reporter reporter) throws IOException {
      // Close current writer
      closeCurrentWriterIfNeeded();
      // Move output files from tmp output path to final output path
      for (Integer partition : writtenPartitions) {
        fs.rename(new Path(tmpOutputPath + "/" + partition),
            new Path(finalOutputPath + "/" + partition));
      }
      // Delete tmp output path
      Path tmpOutputPathObject = new Path(tmpOutputPath);
      if (fs.listStatus(tmpOutputPathObject).length == 0) {
        fs.delete(tmpOutputPathObject);
      } else {
        throw new RuntimeException("Temporary record writer directory was not empty after moving all written partitions: " + tmpOutputPath);
      }
      // Delete tmp output path parent if empty
      Path tmpOutputPathParent = tmpOutputPathObject.getParent();
      if (fs.listStatus(tmpOutputPathParent).length == 0) {
        fs.delete(tmpOutputPathParent);
      }
    }

    @Override
    public void write(IntWritable partitionWritable,
        HankRecordWritable record) throws IOException {
      Integer partition = Integer.valueOf(partitionWritable.get());
      // If writing a new partition, get a new writer
      if (writerPartition == null ||
          writerPartition != partition) {
        // Set up new writer
        setNewPartitionWriter(partition);
      }
      // Write record
      writer.write(ByteBuffer.wrap(record.getKey()), ByteBuffer.wrap(record.getValue()));
    }

    private void setNewPartitionWriter(int partition) throws IOException {
      // First, close current writer
      closeCurrentWriterIfNeeded();
      // Check for existing partitions
      if (writtenPartitions.contains(partition)) {
        throw new RuntimeException("Partition " + partition + " has already been written.");
      }
      // Set up new writer
      // TODO: deal with versions
      // TODO: deal with base/non base
      int versionNumber = 1;
      writer = storageEngine.getWriter(tmpOutputStreamFactory, partition, versionNumber, true);
      writerPartition = partition;
      writtenPartitions.add(partition);
    }

    private void closeCurrentWriterIfNeeded() throws IOException {
      if (writer != null) {
        writer.close();
      }
    }
  }
}
