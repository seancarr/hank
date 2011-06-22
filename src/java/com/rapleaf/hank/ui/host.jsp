<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@page import="com.rapleaf.hank.coordinator.*"%>
<%@page import="com.rapleaf.hank.ui.*"%>
<%@page import="java.util.*"%>

<%
  Coordinator coord = (Coordinator)getServletContext().getAttribute("coordinator");

RingGroup ringGroup = coord.getRingGroupConfig(request.getParameter("g"));

Ring ring = ringGroup.getRing(Integer.parseInt(request.getParameter("r")));
Host host = ring.getHostByAddress(PartDaemonAddress.parse(URLEnc.decode(request.getParameter("h"))));
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>Host: <%= host.getAddress() %></title>
  <jsp:include page="_head.jsp" />

  <style type="text/css">
    td.unassigned {background-color: #ddd}
    td.undeployed {background-color: #f00}
    td.updating {background-color: #00f}
    td.updated {background-color: #0f0}

    div.part_assignment_visualization {float:left; padding: 3px}
  </style>
</head>
<body>

<h3>
  <a href="/ring_group.jsp?name=<%=ringGroup.getName() %>"><%= ringGroup.getName() %></a>
  &gt;
  <a href="/ring.jsp?g=<%=ringGroup.getName() %>&n=<%= ring.getRingNumber() %>">ring <%= ring.getRingNumber() %></a>
  &gt; <%= host.getAddress() %>
</h3>

<div>
  <h3>Status</h3>
  Currently <%= host.getState() %> <%= host.isOnline() ? "(online)" : "" %><br/>
  Current command: <%= host.getCurrentCommand() %> <br/>
  Queued commands: <%= host.getCommandQueue() %> <br/>
  <form action="/host/enqueue_command" method="post">
    <input type="hidden" name="g" value="<%= ringGroup.getName() %>"/>
    <input type="hidden" name="n" value="<%= ring.getRingNumber() %>"/>
    <input type="hidden" name="h" value="<%= host.getAddress() %>"/>

    <select name="command">
    <% for (HostCommand cmd : HostCommand.values()) {%>
      <option><%= cmd.name() %></option>
    <% } %>
    </select>
    <input type="submit" value="Enqueue Command"/>
  </form>
</div>

<div>
  <h4>Domains + Parts</h4>
  <form method="post" action="/host/add_domain_part">
    <input type="hidden" name="g" value="<%= ringGroup.getName() %>"/>
    <input type="hidden" name="n" value="<%= ring.getRingNumber() %>"/>
    <input type="hidden" name="h" value="<%= host.getAddress() %>"/>

    Add a domain/part:<br/>
    <select name="domainId">
      <%
        for (Domain domain : ringGroup.getDomainGroup().getDomains()) {
      %>
      <option value="<%=ringGroup.getDomainGroup().getDomainId(domain.getName())%>"><%=domain.getName()%></option>
      <%
        }
      %>
    </select>
    Part:<input type=text size=4 name="partNum" />
    Initial Version:<input type=text size=4 name="initialVersion"/>
    <input type=submit value="Add"/>
  </form>

  <div style="border: 1px solid #ddd; width: 200px">
    <table style="font-size: 8px">
      <tr>
        <td colspan=2 align=center style="border-bottom: 1px solid #ddd">Legend</td>
      </tr>
      <tr>
        <td class="unassigned" style="width:6px; height: 6px; font-size:0px">&nbsp;</td>
        <td>Not assigned</td>
      </tr>
      <tr>
        <td class="undeployed" style="width:6px; height: 6px; font-size:0px">&nbsp;</td>
        <td>Assigned, no version deployed</td>
      </tr>
      <tr>
        <td class="updating" style="width:6px; height: 6px; font-size:0px">&nbsp;</td>
        <td>Assigned, some version deployed, update pending</td>
      </tr>
      <tr>
        <td class="updated" style="width:6px; height: 6px; font-size:0px">&nbsp;</td>
        <td>Assigned, latest version deployed</td>
      </tr>
    </table>
  </div>
  <%
    List<HostDomain> hostDomains = new ArrayList<HostDomain>(host.getAssignedDomains());
    Collections.sort(hostDomains);
    for (HostDomain hdc : hostDomains) {
      Domain domain = ringGroup.getDomainGroup().getDomain(hdc.getDomainId());
      int squareDim = (int)Math.floor(Math.sqrt(domain.getNumParts()));
  %>
  <div class="part_assignment_visualization">
    <div><%= domain.getName() %></div>
    <div>
      <table cellspacing=1 cellpadding=0>
      <% 
      for (int i = 0; i < domain.getNumParts(); i++) { 
        String className = "unassigned";
        HostDomainPartition hdp = hdc.getPartitionByNumber(i);
        if (hdp != null) {
          if (hdp.getCurrentDomainGroupVersion() == null) {
            className = "undeployed";
          } else if (hdp.getUpdatingToDomainGroupVersion() != null) {
            className = "updating";
          } else {
            className = "updated";
          }
        }
      %>
        <% if (i % squareDim == 0) { %>
        <tr>
        <% } %>
          <td class="<%= className %>" style="font-size: 0px; width: 4px; height: 4px;" title="<%= i %>">&nbsp;</td>
        <% if (i % squareDim == squareDim - 1) { %>
        </tr>
        <% } %>
      <% } %>
      </table>
    </div>
  </div>
  <% } %>


  <div style="clear:both"></div>

  <table class="table-blue">
    <tr><th>domain</th><th>part #</th><th>cur ver #</th><th>upd ver #</th></tr>
  <%
    hostDomains = new ArrayList<HostDomain>(host.getAssignedDomains());
    Collections.sort(hostDomains);
    for (HostDomain hdc : hostDomains) {
  %>
    <tr>
      <th><%=ringGroup.getDomainGroup().getDomain(hdc.getDomainId()).getName()%></th>
    </tr>
    <%
      for (HostDomainPartition hdpc : new TreeSet<HostDomainPartition>(hdc.getPartitions())) {
    %>
    <tr>
      <td></td>
      <td><%= hdpc.getPartNum() %></td>
      <td><%= hdpc.getCurrentDomainGroupVersion() %></td>
      <td><%= hdpc.getUpdatingToDomainGroupVersion() %></td>
    </tr>
    <% } %>
  <% } %>
  </table>
</div>

<div>

</div>
  <h4>Counters</h4>
  <table class="table-blue">
    <tr><th>Counter Name</th><th>Count</th>
    <%
      for (HostDomain hdc : hostDomains) {
    %>	
      <tr>
        <th><%=ringGroup.getDomainGroup().getDomain(hdc.getDomainId()).getName()%></th>
      </tr>
      <% 
        for (HostDomainPartition hdpc : new TreeSet<HostDomainPartition>(hdc.getPartitions())) {
          for (String currentCountKey : hdpc.getCountKeys()) {
      %>
      <tr>
        <td> <%=currentCountKey%> </td>
        <td> <%=hdpc.getCount(currentCountKey)%> </td>
      </tr>
        <% } %>
      <% } %>
    <% } %>
  </table>
</body>
</html>
