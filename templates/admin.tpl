<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <title>{{ title }}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
    <link href="/s/css/admin.css?{VERSION}" rel="stylesheet" type="text/css" />
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
    <ul id="commands">
      <li>
        <a href="/admin">Refresh</a>
      </li>
      <li>
        {{#fetcher}}
          <a href="/admin/fetcher?command=stop">stop fetcher</a>
        {{/fetcher}}
        {{^fetcher}}
        <a href="/admin/fetcher?command=start">start fetcher</a>
        {{/fetcher}}
      </li>
    </ul>
  </body>
</html>
