<!doctype html>
<html>
  <head>
    {{>partials/m_header}}
    <link href="/s/css/m.css?{VERSION}" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <h2>{{title}} <span class="c">{{ category }}</span></h2>
    <ul class="lists">
      {{#feeds}}
        <li {{#read?}}class=r{{/read?}}>
          <p>
            <a href="/m/r/{{id}}">{{ title }}</a>
          </p>
          <div class="meta">
            <i>{{ pts }}</i>
            <i>{{ author }}</i>
            {{#tags}}<i class=t>{{.}}</i>{{/tags}}
          </div>
        </li>
      {{/feeds}}
    </ul>
  </body>
  <!-- <script src="/s/js/sq/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
