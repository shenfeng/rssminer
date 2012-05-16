{{#feeds}}
  <li class="feed {{cls}}" id="feed-{{id}}" data-id="{{id}}">
    <a href="#{{href}}">
      <span class="indicator"></span>
      <span class="title">{{title}}</span>
      {{#author}}
        <!-- <span class="author">{{author}}</span> -->
      {{/author}}
      <span class="date">{{ date }}</span>
    </a>
  </li>
{{/feeds}}
