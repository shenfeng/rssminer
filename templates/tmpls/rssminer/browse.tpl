<div class="lfloat" id="feeds">
  <ul>
    {{#feeds}}
      <li class="feed" data-docid={{docId}} data-feedid={{feedid}}>
        <div class="related">
          <span>similar</span>
        </div>
        <h3>{{title}}</h3>
        <div class="clearfix">
          <span class="author">
            by <a href="/browse/author:{{authorTag}}">{{author}}</a>
          </span>
          <ul class="tags rfloat">
            {{#categories}}
              <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
            {{/categories}}
          </ul>
        </div>
        <p class="snippet">{{snippet}}</p>
      </li>
    {{/feeds}}
  </ul>
</div>

<div id="right-side">
  <div id="tags">
    <p class="section-title">By Tag</p>
    <ul>
      <li><a href="/browse">All</a></li>
      {{#tags}}
        <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
      {{/tags}}
    </ul>
  </div>
  <div id="links">
    <p class="section-title">Links</p>
    <ul>
      <li><a href="/">Landing</a></li>
      <li><a href="http://shenfeng.me">Blog</a></li>
      <li>
        <a href="https://github.com/shenfeng/rssminer">Code</a>
      </li>
    </ul>
  </div>
</div>

