<% if(pending_links.length > 0) { %>
<table id="pending-links">
  <caption>Pending Links</caption>
  <tr>
    <th> url </th>
    <th> last check </th>
    <th> check interval </th>
    <th> referer </th>
  </tr>
  <% _.each(pending_links, function(link) { %>
  <tr>
    <td><%= link.url%></td>
    <td><%= _.timesince(link.check_ts)%></td>
    <td><%= _.interval(link.check_interval)%></td>
    <td><%= link.referer%></td>
  </tr>
  <%}); %>
</table>
<%} %>

<% if(crawled_links.length > 0) { %>
<caption>Crawled Links</caption>
<table id="crawled-links">
  <tr>
    <th> url </th>
    <th> last check </th>
    <th> check interval </th>
    <th> referer </th>
  </tr>
  <% _.each(crawled_links, function(link) { %>
  <tr>
    <td><%= link.url%></td>
    <td><%= _.timesince(link.check_ts)%></td>
    <td><%= _.interval(link.check_interval)%></td>
    <td><%= link.referer%></td>
  </tr>
  <%}); %>
</table>
<%} %>
