package org.cufy.sample;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.cufy.http.middleware.JSONMiddleware;
import org.cufy.http.middleware.OkHttpMiddleware;
import org.cufy.http.sink.Sink;
import org.cufy.http.sink.SkipSink;
import org.cufy.infiniterecyclerviewadapter.InfiniteRecyclerViewAdapter;
import org.cufyx.http.connect.XClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {
	/**
	 * The items getter sink.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected final Sink sink = new SkipSink();
	/**
	 * The recycler view adapter.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected InfiniteRecyclerViewAdapter adapter;
	/**
	 * The layout manager of the recycler view.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected LinearLayoutManager manager;
	/**
	 * True, there are more items to be loaded. False, otherwise.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected boolean moreToLoad = true;
	/**
	 * The last loaded page.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected int page = 1;
	/**
	 * The recycler view.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected RecyclerView vRecyclerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);

		//references
		this.vRecyclerView = this.findViewById(R.id.recyclerView);
		//control
		this.manager = new LinearLayoutManager(this);
		this.adapter = new InfiniteRecyclerViewAdapter();
		//setup
		this.vRecyclerView.setAdapter(this.adapter);
		this.vRecyclerView.setLayoutManager(this.manager);
		//listeners
		this.adapter.<JSONObject>setItemAutoViewHolderBinder(R.layout.card_item, (holder, item) -> {
			assert item != null;
			holder.<TextView>findViewById(R.id.name)
					.setText(item.optString("title"));
		});
		this.adapter.addOnScrollListener((count, first, last) -> {
			if (last + 1 == count)
				this._loadMore();
		});

		this._loadMore();
	}

	/**
	 * Load more items to the recycler view.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	protected void _loadMore() {
		if (!this.moreToLoad)
			return;

		String uri = this.getString(R.string.api_v2_items);
		XClient.defaultClient(this)
			   .request(r -> r
					   .uri(uri)
					   .query(q -> q
							   .put("page", Integer.toString(this.page))
					   )
			   )
			   .middleware(OkHttpMiddleware.middleware())
			   .middleware(JSONMiddleware.middleware())
			   .onh(JSONMiddleware.CONNECTED, (client, response) -> {
				   JSONObject values = response.body().values();
				   JSONObject data = values.getJSONObject("data");
				   JSONArray items = data.getJSONArray("items");

				   List<JSONObject> list = IntStream.range(0, items.length())
													.mapToObj(items::optJSONObject)
													.collect(Collectors.toList());
				   boolean moreToLoad = data.optBoolean("more_to_load");

				   this.moreToLoad = moreToLoad;
				   this.page++;
				   this.adapter.items().addAll(list);
				   this.sink.flush();
			   })
			   .on("exception|disconnected", (client, throwable) ->
					   this.sink.flush()
			   )
			   .connect(this.sink);
	}
}
