<% if(pending_links.length > 0) { %>
<table>
  <caption>Pending Links</caption>
  <tr>
    <th class="id"> ID </th>
    <th class="url code"> url </th>
    <th> last check </th>
    <th> check interval </th>
    <th class="url code"> referer </th>
  </tr>
  <% _.each(pending_links, function(link) { %>
  <tr>
    <td class="id"><%= link.id%></td>
    <td class="url code"><%= link.url%></td>
    <td><%= _.timesince(link.check_ts)%></td>
    <td><%= _.interval(link.check_interval)%></td>
    <td class="url code"><%= link.referer%></td>
  </tr>
  <%}); %>
</table>
<%} %>

