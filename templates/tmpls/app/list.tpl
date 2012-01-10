{{#feeds}}
  <li class="feed {{cls}}" data-link={{link}}
    id="feed-{{id}}" data-id="{{id}}">
    <a href="#{{href}}">
      <span class="indicator"></span>
      <span class="title">{{title}}</span>
      {{#author}}
        <span class="author">{{author}}</span>
      {{/author}}
      <ul class="tags">
        {{#tags}}
          <li>{{.}}</li>
        {{/tags}}
      </ul>
      <span class="vote">
        <span class="up" title="I like it"></span>
        <span class="down"></span>
      </span>
      <span class="date">{{ date }}</span>
    </a>
  </li>
{{/feeds}}
