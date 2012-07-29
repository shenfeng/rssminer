<div class="feed {{ cls }}" data-id={{ id }}>
  <h2>
    <a href="{{ link }}" target="_blank">{{ title }}</a>
  </h2>
  <span class="date">{{ date }}</span>
  <span class="author">{{ author }}</span>
  {{#tags.length}}
    <ul class="tags">
      {{#tags}}
        <li>{{ . }}</li>
      {{/tags}}
    </ul>
  {{/tags.length}}
  <i class="thumbs">
    <i class="icon-thumbs-up"
      title="like it, give me more like this">
    </i>
    <i class="icon-thumbs-down"
      title="I would rather not seeing it">
    </i>
  </i>
</div>
<div class="summary">
  {{{summary}}}
</div>

