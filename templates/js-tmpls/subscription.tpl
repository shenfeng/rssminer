<div id="content-header">
  <span id="view-style">Show:
    <span id="view-expanded" class="link">Expanded</span>
    <span id="view-list" class="link link-selected">List</span>
  </span>
  <span class="feed-title">{{title}}</span>
</div>
<ul id="entries">
  {{#items}}
  <li class="entry">
    <div class="collapsed">
      <span class="icon"></span>
      <div class="entry-date">{{ymdate published_ts}}</div>
      <div class="entry-main">
        <h4>{{title}}</h4> -
        <span class="snippet">{{snippet}}</span>
      </div>
    </div>
    <div class="entry-container">
      <div class="entry-main">
        <h2 class="entry-title">{{title}}</h2>
        <span class="entry-author">{{author}}</span>
        <div class="entry-body">
          {{{summary}}}
        </div>
      </div>
      <div class="entry-comments">
      </div>
    </div>
  </li>
  {{/items}}
</ul>
