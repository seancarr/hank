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
package com.rapleaf.hank.cli;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.rapleaf.hank.config.ClientConfigurator;
import com.rapleaf.hank.config.YamlConfigurator;

public class AddDomain {
  private final ClientConfigurator configurator;

  public AddDomain(ClientConfigurator configurator) {
    this.configurator = configurator;
  }

  public void addDomain(String domainName, String numParts, String factoryName, String factoryOptions, String partitionerName, String version)
  throws InterruptedException, IOException {
    configurator.getCoordinator().addDomain(domainName,
        Integer.parseInt(numParts),
        factoryName,
        factoryOptions,
        partitionerName,
        Integer.parseInt(version));
  }

  public static void main(String[] args) throws InterruptedException, ParseException, IOException {
    Options options = new Options();
    options.addOption("n", "name", true,
        "the name of the domain to be created");
    options.addOption("p", "num-parts", true,
        "the number of partitions for this domain");
    options.addOption("f", "storage-engine-factory", true,
        "class name of the storage engine factory used by this domain");
    options.addOption("o", "storage-engine-options", true,
        "path to a yaml file containing the options for the storage engine");
    options.addOption("t", "partitioner", true,
        "class name of the partition used by this domain");
    options.addOption("v", "initial-version", true,
        "initial version number of this domain");
    options.addOption("c", "config", true,
        "path of a valid config file with coordinator connection information");
    try {
      CommandLine line = new GnuParser().parse(options, args);
      ClientConfigurator configurator = new YamlConfigurator(line.getOptionValue("config"));
      new AddDomain(configurator).addDomain(line.getOptionValue("name"),
          line.getOptionValue("num-parts"),
          line.getOptionValue("storage-engine-factory"),
          line.getOptionValue("storage-engine-options"),
          line.getOptionValue("partitioner"),
          line.getOptionValue("initial-version"));
    } catch (ParseException e) {
      new HelpFormatter().printHelp("add_domain", options);
      throw e;
    }
  }
}
