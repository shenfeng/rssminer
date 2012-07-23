<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>title</title>
    <link href="/css/compare.css" rel="stylesheet" type="text/css" />
    <style tyle="text/css">
    .links li { display: inline-block; padding: 0 10px; }
    </style>
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
          <th class="id">compact_len</th>
          <th class="compact"> summary </th>
          <th class="summary"> compact</th>
        </tr>
        {{#feeds}}
          <tr>
            <td>
              <a target="_blank"
                href="{{ link }}">{{ id }}</a>
            </td>
            <td>{{ summary_length }}</td>
            <td>{{ compact_lenght }}</td>
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
  </body>
</html>
