<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>title</title>
    <link href="/s/css/m.css" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <ul class="lists">
      {{#feeds}}
        <li>
          <p>
            <a href="/m/f/{{id}}">{{ title }}</a>
          </p>
          <div class="meta">
            <span>{{ pts }}</span>
            <span>{{ author }}</span>
            <span>{{ tags }}</span>
          </div>
        </li>
      {{/feeds}}
    </ul>
  </body>
  <!-- <script src="/s/js/sq/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
