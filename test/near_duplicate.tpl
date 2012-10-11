<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>Near duplicate</title>
    <link href="/static/tools.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <div id="page-wrap">
      <ul class="pager">
        {{#pages}}
          <li>
            <a href="/s?id={{.}}">{{ . }}</a>
          </li>
        {{/pages}}
      </ul>
      <ul id="near-duplicate">
        {{#article}}
          <li class="article">
            <h3>
              <a href="{{link}}">[{{rss_link_id}}:{{id}}] {{ title }} </a>
            </h3>
            <span class="link">{{ link }}</span>
            <div class="summary">
              {{{summary}}}
            </div>
          </li>
        {{/article}}
        {{#similars}}
          <li class="duplicate">
            <h3>
              <a href="{{link}}"> [{{rss_link_id}}:{{id}}] {{ title }}</a>
            </h3>
            <span class="link">{{ link }}</span>
            <div class="summary">
              {{{summary}}}
            </div>
          </li>
        {{/similars}}
      </ul>
    </div>
  </body>
</html>
