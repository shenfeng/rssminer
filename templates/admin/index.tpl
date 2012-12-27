<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
      {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    {{> admin/nav }}
    <table class="subs">
      {{#subs}}
        <tr>
          <td>{{ id }}</td>
          <td>{{ total_feeds }}</td>
          <td>{{ user_id }}</td>
          <!-- <td>{{ error_msg }}</td> -->
          <td>{{ url }}</td>
        </tr>
      {{/subs}}
    </table>
    <table>
      <caption>Fetcher Stat</caption>
      {{#stat}}
        <tr>
          <td>{{ key }}</td>
          <td>{{ val }}</td>
        </tr>
      {{/stat}}
    </table>
  </body>
</html>
