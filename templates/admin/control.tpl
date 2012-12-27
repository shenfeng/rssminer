<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    {{#admin-css}}<style type="text/css">{{{admin-css}}}</style>{{/admin-css}}
      {{^admin-css}}<link rel="stylesheet" href="/s/css/admin.css">{{/admin-css}}
  </head>
  <body>
    {{#ok}}
      <div id="notification">
        <p>ok</p>
      </div>
    {{/ok}}
    {{> admin/nav }}
    <table id="services">
      <tr>
        <td>
          <h4>Fetcher</h4>
          <form method="post" action="/admin/control?kind=fetcher">
          {{#fetcher}}
            <input type="submit" name="command" value="stop"/>
          {{/fetcher}}
          {{^fetcher}}
          <input type="submit" name="command" value="start"/>
          {{/fetcher}}
          </form>
        </td>
        <td>
          <h4>Searcher</h4>
          <form method="post" action="/admin/control?kind=searcher">
          {{#searcher}}
            <input type="submit" name="command" value="stop"/>
          {{/searcher}}
          {{^searcher}}
          <input type="submit" name="command" value="start"/>
          {{/searcher}}
          </form>
        </td>
        <td>
          <h4>Classfier</h4>
          <form method="post" action="/admin/control?kind=classfier">
          {{#classfier}}
            <input type="submit" name="command" value="stop"/>
          {{/classfier}}
          {{^classfier}}
          <input type="submit" name="command" value="start"/>
          {{/classfier}}
          </form>
        </td>
      </tr>
    </table>
    <h4>Scores</h4>
    <form method="post" action="/admin/control?kind=score">
    <input type="hidden" name="kind" value="score"/>
    <input class="txt" placeholder="user-id or all" name="command"/>
    <input type="submit" value="recompute"/>
    </form>

    <h4>Refetch Subscription</h4>
    <form method="post" action="/admin/control?kind=subs">
    <input class="txt" placeholder="subid" name="command"/>
    <input type="submit" value="refetch"/>
    </form>
  </body>
</html>
