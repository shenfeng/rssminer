<table>
  <caption>{{caption}}</caption>
  <tr>
    <th class="id">ID</th>
    <th class="url code">url</th>
    <th class="title">title</th>
    <th class="time">next check</th>
    <th class="time">check interval</th>
    <th class="time">add</th>
    <th class="referer code">referer</th>
  </tr>
  {{#data}}
    <tr>
      <td class="id">{{id}}</td>
      <td class="url code" title="{{url}}">{{url}}</td>
      <td class="title" title="{{title}}">{{title}}</td>
      <td class="time">{{interval next_check_ts}}</td>
      <td class="time">{{check_interval}}</td>
      <td class="time">{{interval added_ts}}</td>
      <td class="referer code" title="{{referer}}">{{referer}}</td>
    </tr>
  {{/data}}
</table>
