<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>title</title>
    <link href="/static/compare.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <div id="page-wrap">
      <ul class="links">
        {{#links}}
          <li><a href="/compare?start={{.}}">{{ . }}</a></li>{{/links}}
      </ul>
      <table border=1>
        <tr>
          <th class="id">id </th>
          <th class="id">su_len</th>
          <th class="id">c_len</th>
          <th class="compact"> summary </th>
          <th class="summary"> compact</th>
        </tr>
        {{#feeds}}
          <tr>
            <td class="id">
              <a target="_blank" title="{{title}}"
                href="{{ link }}">{{ id }}</a>
            </td>
            <td class="length">{{ summary_length }}</td>
            <td class="length">{{ compact_lenght }}</td>
            <td class="summary">
              {{{ summary }}}
            </td>
            <td class="compact">
              {{{ compact }}}
            </td>
          </tr>
        {{/feeds}}
      </table>
      <ul class="links">
        {{#links}}
          <li><a href="/compare?start={{.}}">{{ . }}</a></li>{{/links}}
      </ul>
    </div>
    <script src="/static/compare.js"></script>
  </body>
</html>
