<div id="navs">
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

<div id="feeds">
  <ul>
    {{#feeds}}
      <li class="feed" data-id={{id}}>
        <table>
          <tr>
            <td class="ctrls">
              <span class="vote-up {{like}}">▲</span>
              <span class="vote-down {{dislike}}">▼</span>
            </td>
            <td class="data">
              <h3>{{title}}</h3>
              <div class="meta">
                {{#author}}
                  <span class="author">
                    by <a href="/browse/author:{{authorTag}}">{{author}}</a>
                  </span>
                {{/author}}
                <span class="related">
                  <a href="/browse/related:{{docid}}">similar</a>
                </span>
                <ul class="tags">
                  {{#tags}}
                    <li><a href="/browse/tag:{{.}}">{{.}}</a></li>
                  {{/tags}}
                </ul>
              </div>
              <div class="content">
                <p class="snippet">{{snippet}} ...</p>
                <div class="summary"></div>
              </div>
            </td>
          </tr>
        </table>
      </li>
    {{/feeds}}
  </ul>
</div>


