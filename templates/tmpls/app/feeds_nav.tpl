<h3 title="{{title}}">
  {{#url}}
    <a target="blank" title="{{title}}" href="{{url}}">{{title}}</a>
  {{/url}}
  {{^url}}
  {{ title }}
  {{/url}}
</h3>
<ul id="feed-list" class="feeds">
  {{#feeds}}
    <li class="feed {{cls}}" id="feed-{{id}}" data-id="{{id}}">
      <a href="#{{href}}">
        <span class="indicator"></span>
        <span class="title">{{{title}}}</span>
        {{#author}}
          <!-- <span class="author">{{author}}</span> -->
        {{/author}}
        <span class="date">{{ date }}</span>
      </a>
    </li>
  {{/feeds}}
</ul>
{{#pager}}
  <div id="nav-pager" class="pager">
    {{#prev}}
      <li class="prev">prev</li>
    {{/prev}}
    {{#next}}
      <li class="next">next</li>
    {{/next}}
  </div>
{{/pager}}
