<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
    {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    <table>
      <caption>Fetcher Stat</caption>
      {{#stat}}
        <tr>
          <td>{{ key }}</td>
          <td>{{ val }}</td>
        </tr>
      {{/stat}}
    </table>
    {{#table-stats}}
      <table>
        <caption>{{ name }}</caption>
        {{#stat}}
          <tr>
            <td>{{ key }}</td>
            <td>{{ val }}</td>
          </tr>
        {{/stat}}
      </table>
    {{/table-stats}}
    <ul id="commands">
      <li>
        {{#fetcher}}
          <a href="/admin/fetcher?command=stop">stop fetcher</a>
        {{/fetcher}}
        {{^fetcher}}
        <a href="/admin/fetcher?command=start">start fetcher</a>
        {{/fetcher}}
      </li>
      <li>
        <a href="/admin/compute">recompute scores</a>
      </li>
    </ul>
  </body>
</html>
