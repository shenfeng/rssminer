<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
      {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    {{> admin/nav }}

    <h3>Feedbacks</h3>

    <table>
      {{#feedbacks}}
        <tr>
          <!-- <td>{{ id }}</td> -->
          <!-- <td>{{ email }}</td> -->
          <!-- <td>{{ ip }}</td> -->
          <!-- <td>{{ user_id }}</td> -->
          <td class="date">{{ added_ts }}</td>
          <td>{{ feedback }}</td>
        </tr>
      {{/feedbacks}}
    </table>

  </body>
</html>
