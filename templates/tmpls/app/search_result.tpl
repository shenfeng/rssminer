<div id="search-result">
  {{#sub_cnt}}
    <ul class="subs">
      {{#subs}}
        <li>
          <a href="#{{href}}">
            <img src="{{img}}" width="15" height="15" />
            <span class="title"> {{title}} </span>
            <span class="count">
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
              <span class="total" title="total feed">{{ total }}</span>
            </span>
          </a>
        </li>
      {{/subs}}
    </ul>
  {{/sub_cnt}}
  <ul class="feeds">
    {{#feeds}}
      <li class="feed {{cls}}">
        <a href="#{{href}}">
          <span class="indicator"></span>
          <span class="title" title="{{title}}">{{title}}</span>
          <div class="meta">
            <span class="author" title="author">{{author}}</span>
            <ul class="tags">
              {{#tags}}
                <li title="tag">{{.}}</li>
              {{/tags}}
            </ul>
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
