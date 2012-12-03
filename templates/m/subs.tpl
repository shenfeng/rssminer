<!doctype html>
<html>
  {{>m/p_header}}
  <body>
    <h2>{{m-sub-list}}</h2>
    <ul class="lists">
      {{#subs}}
        <li>
          <p>
            <a href="/m/{{id}}"> {{ title }} </a>
          </p>
          <div class="meta">
            <i>猜你喜欢{{ like }}</i>
            <i>共{{ total }}</i>
          </div>
        </li>
      {{/subs}}
    </ul>
  </body>
  <!-- <script src="/s/js/lib/zepto.js"></script> -->
  <!-- <script src="/s/js/rssminer/mobile.js"></script> -->
</html>
