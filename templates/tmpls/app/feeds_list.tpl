{{#feeds}}
  <li class="feed {{cls}}" id="feed-{{id}}" data-id="{{id}}">
    <a href="#{{href}}">
      <span class="indicator"></span>
      <span class="title">{{{title}}}</span>
      <div>
        <span class="date">{{ date }}</span>
        {{#author}}
          <span class="author">{{author}}</span>
        {{/author}}
      </div>
    </a>
  </li>
{{/feeds}}
