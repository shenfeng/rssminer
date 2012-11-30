{{#feeds}}
  <li id="s-{{id}}" data-id={{id}}>
    <div class="feed {{cls}}" data-id={{id}}>
      <h2><a href="{{link}}" target="_blank">{{title}}</a></h2>
      <a class="link" href="{{link}}" target="_blank">{{{link_d}}}</a>
      <div class="meta">
        <i class="thumbs">
          <i class="icon-thumbs-up"
            data-title="{{m_like_title}}">
          </i>
          <i class="icon-thumbs-down"
            data-title="{{m_dislike_title}}">
          </i>
        </i>
        <span data-title="author" class="author">{{author}}</span>
        {{#sub}}
          <div class="sub" data-title="subscription">
            <a href="#{{{href}}}">{{title}}</a>
          </div>
        {{/sub}}
        {{#tags.length}}
          <ul class="tags" data-title="tag">
            {{#tags}}
              <li>{{.}}</li>
            {{/tags}}
          </ul>
        {{/tags.length}}
        <span class="date">{{m_publish}} {{date}}</span>
        {{#rdate}}
          <span class="date">{{m_read}} {{rdate}}</span> <!-- read date -->
        {{/rdate}}
      </div>
    </div>
    <div class="summary">{{{summary}}}</div>
  </li>
{{/feeds}}
