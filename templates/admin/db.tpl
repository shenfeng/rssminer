<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
      {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    {{> admin/nav }}
    {{#table-stats}}
      <table class="db">
        <caption>{{ name }}</caption>
        {{#stat}}
          <tr>
            <td>{{ key }}</td>
            <td>{{ val }}</td>
          </tr>
        {{/stat}}
      </table>
    {{/table-stats}}
  </body>
</html>
