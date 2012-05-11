<div id="search-result">
  <ul class="subs">
    {{#subs}}
      <li>
        <a href="#{{href}}">
          <img src="{{img}}" width="15" height="15" />
          <span class="title"> {{title}} </span>
          <span class="count">
            <span class="total" title="total feed">{{ total }}</span>
            {{#dislike}}
              <span class="unread-dislike"
                title="dislike count">{{dislike}}</span>
            {{/dislike}}
            {{#neutral}}
              <span class="unread-neutral"
                title="neutral count">{{neutral}}</span>
            {{/neutral}}
            {{#like}}
              <span class="unread-like"
                title="like count">{{like}}</span>
            {{/like}}
          </span>
        </a>
      </li>
    {{/subs}}
  </ul>
  <ul class="feeds">
    {{#feeds}}
      <li class="feed {{cls}}">
        <a href="#{{href}}">
          <span class="indicator"></span>
          <span class="title" title="{{title}}">{{title}}</span>
          <div class="meta">
            <ul class="tags">
              {{#tags}}
                <li title="tag">{{.}}</li>
              {{/tags}}
            </ul>
            {{#author}}
              <span class="author" title="author">{{author}}</span>
            {{/author}}
            {{#sub}}
              <span class="sub" title="from">{{ title }}</span>
            {{/sub}}
            <span class="date">{{ date }}</span>
          </div>
        </a>
      </li>
    {{/feeds}}
  </ul>
</div>
