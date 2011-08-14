<% if(rss_links.length > 0) { %>
<table id="crawled-links">
  <caption>Rss Links</caption>
  <tr>
    <th class="id"> ID </th>
    <th class="url code"> url </th>
    <th class="title"> title </th>
    <th class="added_ts"> added time </th>
    <th class="referer code"> referer </th>
  </tr>
  <% _.each(rss_links, function(link) { %>
  <tr>
    <td class="id"><%= link.id%></td>
    <td class="url code"><%= link.url%></td>
    <td class="title"><%= link.title%></td>
    <td class="added_ts"><%= _.timesince(link.added_ts)%></td>
    <td class="refer code"><%= link.referer%></td>
  </tr>
  <%}); %>
</table>
<%} %>
