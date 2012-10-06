<div id="instant-search">
  {{#sub_cnt}}
    <ul class="subs">
      {{#subs}}
        <li class="ficon-error {{ cls }}">
          <a href="#{{href}}">
            {{#is_group}}
              <i class="icon-folder-open"></i>
            {{/is_group}}
            {{^is_group}}
            <img src="{{img}}" width="15" height="15" />
            <i class="icon-rss"></i>
            {{/is_group}}
            <span class="title">{{{title}}}</span>
            <span class="count">
              {{#dislike}}
                <span class="unread-dislike"
                  data-title="dislike count">{{dislike}}</span>
              {{/dislike}}
              {{#neutral}}
                <span class="unread-neutral"
                  data-title="neutral count">{{neutral}}</span>
              {{/neutral}}
              {{#like}}
                <span class="unread-like"
                  data-title="like count">{{like}}</span>
              {{/like}}
              <span class="total" data-title="total feed count">
                {{ total }}
              </span>
            </span>
          </a>
        </li>
      {{/subs}}
    </ul>
  {{/sub_cnt}}
  {{#server}}
    <ul class="feeds">
      {{#feeds}}
        <li class="feed {{cls}}">
          <a href="#{{href}}">
            <span class="indicator"></span>
            <span class="title">{{{title_h}}}</span>
            {{#sub}}
              <span class="sub" data-title="from {{ title }}">{{ title }}</span>
            {{/sub}}
          </a>
        </li>
      {{/feeds}}
    </ul>
  {{/server}}
  <ul>
    <li>
      <a href="#search?q={{q}}&tags=&authors=&offset=0">
        search <b>{{q}}</b>
        {{#server}}
          <span class="count"><span class="total">{{ total }}</span></span>
        {{/server}}
      </a>
    </li>
  </ul>
</div>
