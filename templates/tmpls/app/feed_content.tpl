<div class="feed {{ cls }}" data-id={{ id }}>
  <h2>
    <a href="{{ link }}" target="_blank">{{ title }}</a>
  </h2>
  <div class="meta">
    <span title="author" class="author">{{ author }}</span>
    {{#sub}}
      <a class="sub" href="#{{{href}}}">{{title}}</a>
    {{/sub}}
    {{#tags.length}}
      <ul class="tags">
        {{#tags}}
          <li title="tag">{{ . }}</li>
        {{/tags}}
      </ul>
    {{/tags.length}}
    <span class="date">{{ date }}</span>
    <i class="thumbs">
      <i class="icon-thumbs-up"
        title="like it, give me more like this">
      </i>
      <i class="icon-thumbs-down"
        title="I would rather not seeing it">
      </i>
    </i>
  </div>
</div>
<div class="summary">
  {{{summary}}}
</div>

