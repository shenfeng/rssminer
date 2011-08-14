<div id="stats">
  <span>Total <%=total_count%></span>
  <span>Crawled <%=crawled_count%></span>
</div>
<% if(rss_links.length > 0) { %>
 <h2>Rss Links</h2>
 <table id="crawled-links">
   <tr>
     <th> url </th>
     <th> title </th>
     <th> add_ts </th>
     <th> referer </th>
   </tr>
   <% _.each(rss_links, function(link) { %>
   <tr>
     <td><%= link.url%></td>
     <td><%= link.title%></td>
     <td><%= _.timesince(link.added_ts)%></td>
     <td><%= link.referer%></td>
   </tr>
   <%}); %>
 </table>
<%} %>
