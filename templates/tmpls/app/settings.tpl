<h2>
  Settings - Rssminer
</h2>
<div class="sort">
  <ul>
    {{#tabs}}
    <li {{#s}} class="selected" {{/s}}>
      <a href="#s/{{ n }}">{{ n }}</a>
    </li>
    {{/tabs}}
  </ul>
</div>
<ul id="all-settings" class="show-{{selected}}">
  <li class="section add-sub">
    <div>
      {{#demo}}
      <p class="demo-warn">
        This is a demo account, please
        <a href="/logout">create an account</a>
        to add subscription.
      </p>
      {{/demo}}
      <section>
        <h4>Import</h4>
        <div>
          <p>
            <a href="/import/google"
               title="Import all you google reader subscriptions:">
              <img src="/imgs/import-greader.png"/>
            </a>
          </p>
        </div>
      </section>
      <section>
        <h4>URL</h4>
        <div>
          <input id="rss_atom_url" placeholder="paste atom/rss url here"/>
          <ul class="help">
            <li>
              <a target="_blank"
                 title="RSS - Wikipedia, the free encyclopedia"
                 href="http://en.wikipedia.org/wiki/RSS">
                <img src="/imgs/wiki.ico"/>
              </a>
            </li>
            <li>
              <a target="_blank" title="简易信息聚合"
                 href="http://baike.baidu.com/view/1644.htm">
                <img src="/imgs/bk.ico"/>
              </a>
            </li>
          </ul>
        </div>
      </section>
      <section>
        <button id="add-subscription">add</button>
      </section>
    </div>
  </li>
  <li class="section account">
    <div>
      <section>
        <h4>Password</h4>
        <div>
          <input type="password" placeholder="new password"
                 name="password"/>
          <br>
          <input type="password" placeholder="retype password"
                 name="password2" id="password2"/>
        </div>
      </section>
      <section>
        <button id="save-settings">Save</button>
      </section>
    </div>
  </li>
  <li class="section about">
    <div>
      <h3>What are the colors for?</h3>
      <p>All the unread articles are sorted.</p>
      <ul>
        <li class="like">
          <span class="bar"></span>
          The Top 20%. Having little time? try read them first.
          <i class="icon-thumbs-down"></i>
          If you want to correct Rssminer.
        </li>
        <li class="neutral">
          <span class="bar"></span>
          20% ~ 50%. If you not in a hurry, they may be the choice
        </li>
        <li class="dislike">
          <span class="bar"></span>
          The remaining 50%.
          <i class="icon-thumbs-up"></i>
          if one actually worth
          reading, Rssminer will learn the lession</li>
      </ul>
      <h3>How articles are sorted?</h3>
      <ul>
        <li>Every unread article is given a score</li>
        <li>The score is computed for you by learn from your recent
          reading history and explit
          vote: <i class="icon-thumbs-down"></i>
          <i class="icon-thumbs-up"></i>
        </li>
        <li>Newer articles trend to have a higher score than older
          ones
        </li>
        <li>Articles are sorted by the score</li>
        <li>The scores are updated as you read or vote, automatically</li>
      </ul>
      <h3>
        Reading Note <i class="icon-comment"></i>,
        Sharing <i class="icon-share"></i>
      </h3>
      <p>I am working on it...</p>
      <h3>Bug report, Feature sugesstion</h3>
      <i class="icon-envelope"></i>Email me: shenedu@gmail.com
      <h3>English Dictionary</h3>
      An <a target="_blank" href="http://dict.shenfeng.me">
        English-English dictionary
      </a>
      to help you reading
      <h3>Blog</h3>
      <a target="_blank" href="http://shenfeng.me">http://shenfeng.me</a>
      <h3>Open source <i class="icon-github"></i> </h3>
      <a target="_blank"
         href="https://github.com/shenfeng/rssminer">
        https://github.com/shenfeng/rssminer
      </a>
    </div>
  </li>
</ul>



