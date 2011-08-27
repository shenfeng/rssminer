<table>
  <caption>Pending Links</caption>
  <tr>
    <th class="id"> ID </th>
    <th class="url code"> url </th>
    <th> last check </th>
    <th> check interval </th>
    <th class="url code"> referer </th>
  </tr>
  {{#pending_links}}
    <tr>
      <td class="id">{{id}}</td>
      <td class="url code">{{url}}</td>
      <td>{{check_ts}}</td>
      <td>{{check_interval}}</td>
      <td class="url code">{{referer}}</td>
    </tr>
  {{/pending_links}}
</table>

