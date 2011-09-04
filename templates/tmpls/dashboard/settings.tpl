<div id="plot">
</div>
<div class="section">
  <h3>server control</h3>
  <table>
    <tr class="crawler">
      <td>Cralwer</td>
      <td>{{crawler_running}} </td>
      <td>
        <button>
          {{#crawler_running}}stop {{/crawler_running}}
            {{^crawler_running}}start {{/crawler_running}}
        </button>
      </td>
    </tr>
    <tr class="fetcher">
      <td>Fetcher</td>
      <td>{{fetcher_running}} </td>
      <td>
        <button>
          {{#fetcher_running}}stop {{/fetcher_running}}
            {{^fetcher_running}}start {{/fetcher_running}}
        </button>
      </td>
    </tr>
  </table>
</div>

<div class="section">
  <h3>statistics</h3>
  <table>
    <tr>
      <th></th>
      <th>Count</th>
      <th>Delta</th>
    </tr>
    <tr>
      <td>Feed:</td>
      <td>{{feeds_count}}</td>
      <td>{{feeds_count_delta}}</td>
    </tr>
    <tr>
      <td>Rss:</td>
      <td>{{rss_links_cout}} </td>
      <td>{{rss_links_count_delta}}</td>
    </tr>
    <tr>
      <td>Rss Fetched:</td>
      <td>{{rss_finished}}</td>
      <td>{{rss_finished_delta}}</td>
    </tr>
    <tr>
      <td>Rss Pending: </td>
      <td>{{rss_pending}}</td>
      <td>{{rss_pending_delta}}</td>
    </tr>
    <tr>
      <td> Links: </td>
      <td>{{crawler_links_count}}</td>
      <td>{{crawler_links_count_delta}}</td>
    </tr>
    <tr>
      <td> Crawled: </td>
      <td>{{crawled_count}}</td>
      <td>{{crawled_count_delta}}</td>
    </tr>
    <tr>
      <td> Pending: </td>
      <td>{{pending_count}}</td>
      <td>{{pending_count_delta}}</td>
    </tr>
  </table>
</div>

<div class="section" id="black-domains">
  <h3>black domain</h3>
  <input />
  <ul>
    {{#black_domains}}
      <li>{{patten}}</li>
    {{/black_domains}}
  </ul>
</div>

<div class="section" id="reseted-domains">
  <h3>reseted domain</h3>
  <input/>
  <ul>
    {{#reseted_domains}}
      <li>{{patten}}</li>
    {{/reseted_domains}}
  </ul>
</div>


