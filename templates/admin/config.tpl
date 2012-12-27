<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
      {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    {{> admin/nav }}
    <table id="configs">
      {{#configs}}
        <tr>
          <td>{{ key }}</td>
          <td>{{ val }}</td>
        </tr>
      {{/configs}}
    </table>
  </body>
</html>
