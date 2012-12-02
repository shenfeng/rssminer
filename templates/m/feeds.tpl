<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
    <title>{{ title }}</title>
    <link href="/s/css/m.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <h2>{{title}}</h2>
    <ul class="lists">
      {{#feeds}}
        <li {{#read?}}class=r{{/read?}}>
          <p>
            <a href="/m/f/{{id}}">{{ title }}</a>
          </p>
          <div class="meta">
            <i>{{ pts }}</i>
            <i>{{ author }}</i>
            <i>{{ tags }}</i>
          </div>
        </li>
      {{/feeds}}
    </ul>
  </body>
  <!-- <script src="/s/js/sq/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
