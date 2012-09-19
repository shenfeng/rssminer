<h3>
  {{#url}}
    <a target="blank" href="{{url}}">{{title}}</a>
  {{/url}}
  {{^url}}
  {{ title }}
  {{/url}}
</h3>
<ul id="feed-list" class="feeds">
  {{>feeds_list}}
</ul>
{{#pager}}
  <img class="loader" src="/s/imgs/loader-15.gif"/>
{{/pager}}
