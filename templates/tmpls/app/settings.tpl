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
    <div id="add-sub">
      <section>
        <h4>Import</h4>
        <div>
          <p>
            <a href="/import/google">
            <img src="/imgs/import-greader.png"/>
            </a>
          </p>
        </div>
      </section>
      <section>
        <h4>URL</h4>
        <div>
          <input id="rss_atom_url" placeholder="atom/rss url"/>
        </div>
      </section>
      <section>
        <button id="add-subscription">add</button>
      </section>
    </div>
  </li>
  <li class="section account">
    <div id="account">
      <section>
        <h4>Password</h4>
        <div>
          <input type="password" placeholder="new password"
          name="password"/>
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
      <h3>关于Rssminer</h3>
      <p>首先感谢选用Rssminer来作为阅读工具。阅读是一件很认真的事。希望Rssminer能尽微薄之力，使此轻松一点， 有效一点。</p>

      <h3>搜索和排序</h3>
      <p>搜索，能快速找到相关文章；排序，能使感兴趣的文章排前面。Rsssminer提供：</p>
      <ul>
        <li>实时搜索（search as you type）</li>
        <li>实时对所有没有读过的文章，按照你个人的阅读历史和对文章的喜好，评分排序
        </li>
        <li>按时间先后排序</li>
      </ul>
      <p>读原文，里面可能还有评论。</p>
      <p>读过是读过，没读是没有读，不标记为已读。</p>
      <h3>阅读笔记</h3>
      <p>Rssminer暂时没有提供做笔记的功能。但并不是我认为这件事情不
        重要。恰恰相反，这件事情很重要。不动笔墨不读书。读书的同时，做做
        笔记，供日后参考。我正在解决这个问题。请稍等
      </p>
      <h3>Bug report， 功能建议</h3>
      <p>
        发邮件给我 shenedu at gmail.com。或者自己改代码
        <a href="https://github.com/shenfeng/rssminer">https://github.com/shenfeng/rssminer</a>
      </p>
    </div>
  </li>
</ul>



