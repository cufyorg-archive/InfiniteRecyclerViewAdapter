/*
 *	Copyright 2021 Cufy
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package org.cufy.infiniterecyclerviewadapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A simplified ready-to-user recycler view adapter.
 *
 * @author LSafer
 * @version 0.0.1
 * @since 0.0.1 ~2021.04.06
 */
public class InfiniteRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	/**
	 * The items.
	 *
	 * @since 0.0.1~2021.04.06
	 */
	@NotNull
	protected final List<Object> items;
	/**
	 * The native on scroll listener.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@NotNull
	protected final RecyclerView.OnScrollListener onScrollListener;
	/**
	 * The listeners to be invoked when a scroll occurs.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@NotNull
	protected final Set<@NotNull OnScrollListener> onScrollListeners = new HashSet<>();
	/**
	 * A map mapping item types with their on-bind-listener.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@NotNull
	protected final Map<@NotNull Integer, @NotNull ViewHolderBinder> viewHolderBinders = new HashMap<>();
	/**
	 * A map mapping item types with their on-create-listener.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@NotNull
	protected final Map<@NotNull Integer, @NotNull ViewHolderSupplier> viewHolderSuppliers = new HashMap<>();
	/**
	 * The supplier that return the type of the item at the position given to it.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@NotNull
	protected ItemViewTypeSupplier itemViewTypeSupplier = (count, position) -> 0;
	/**
	 * The adopted recycler view.
	 *
	 * @since 0.0.1 ~2021.04.06
	 */
	@Nullable
	protected RecyclerView recyclerView;

	/**
	 * Construct a new infinite recycler view adapter.
	 *
	 * @since 0.0.1 ~2021.04.09
	 */
	@SuppressWarnings({"AnonymousInnerClass", "AnonymousInnerClassWithTooManyMethods",
					   "OverlyComplexAnonymousInnerClass"
	})
	public InfiniteRecyclerViewAdapter() {
		ObservableList<Object> items = new ObservableArrayList<>();
		items.addOnListChangedCallback(new ObservableList.OnListChangedCallback<ObservableList<Object>>() {
			@Override
			public void onChanged(ObservableList<Object> sender) {
				InfiniteRecyclerViewAdapter.this.notifyDataSetChanged();
			}

			@Override
			public void onItemRangeChanged(ObservableList<Object> sender, int positionStart, int itemCount) {
				if (itemCount == 1)
					InfiniteRecyclerViewAdapter.this.notifyItemChanged(positionStart);
				else
					InfiniteRecyclerViewAdapter.this.notifyItemRangeChanged(positionStart, itemCount);
			}

			@Override
			public void onItemRangeInserted(ObservableList<Object> sender, int positionStart, int itemCount) {
				if (itemCount == 1)
					InfiniteRecyclerViewAdapter.this.notifyItemInserted(positionStart);
				else
					InfiniteRecyclerViewAdapter.this.notifyItemRangeInserted(positionStart, itemCount);
			}

			@Override
			public void onItemRangeMoved(ObservableList<Object> sender, int fromPosition, int toPosition, int itemCount) {
				if (itemCount == 1)
					InfiniteRecyclerViewAdapter.this.notifyItemMoved(fromPosition, toPosition);
				else
					InfiniteRecyclerViewAdapter.this.notifyDataSetChanged();
			}

			@Override
			public void onItemRangeRemoved(ObservableList<Object> sender, int positionStart, int itemCount) {
				if (itemCount == 1)
					InfiniteRecyclerViewAdapter.this.notifyItemRemoved(positionStart);
				else
					InfiniteRecyclerViewAdapter.this.notifyItemRangeRemoved(positionStart, itemCount);
			}
		});
		this.items = Collections.synchronizedList(items);
		this.onScrollListener = new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
				RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();

				//only using linear layout manager
				if (manager instanceof LinearLayoutManager) {
					LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;

					int count = manager.getItemCount();
					int first = linearLayoutManager.findFirstVisibleItemPosition();
					int last = linearLayoutManager.findLastVisibleItemPosition();

					InfiniteRecyclerViewAdapter.this.onScrollListeners.forEach(listener ->
							listener.onScroll(count, first, last)
					);
				}
			}
		};
	}

	@Range(from = 0, to = Integer.MAX_VALUE)
	@Override
	public int getItemCount() {
		//the `this.items` will never be null and is the items holder
		return this.items.size();
	}

	@Override
	public int getItemViewType(@Range(from = 0, to = Integer.MAX_VALUE) int position) {
		return this.itemViewTypeSupplier.getItemViewType(this.getItemCount(), position);
	}

	@Override
	public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
		Objects.requireNonNull(recyclerView, "recyclerView");
		this.recyclerView = recyclerView;
		recyclerView.addOnScrollListener(this.onScrollListener);
	}

	@Override
	public void onBindViewHolder(@NotNull RecyclerView.ViewHolder holder, @Range(from = 0, to = Integer.MAX_VALUE) int position) {
		Objects.requireNonNull(holder, "holder");
		int type = this.getItemViewType(position);
		ViewHolderBinder binder = this.viewHolderBinders.get(type);

		if (binder != null) {
			Object item = this.items.get(position);

			binder.onBindViewHolder(holder, item);
		}
	}

	@NotNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
		ViewHolderSupplier supplier = this.viewHolderSuppliers.get(viewType);

		if (supplier != null)
			return supplier.onCreateViewHolder(parent);

		throw new IllegalArgumentException("No supplier for type: " + viewType);
	}

	@Override
	public void onDetachedFromRecyclerView(@NotNull RecyclerView recyclerView) {
		Objects.requireNonNull(recyclerView, "recyclerView");
		this.recyclerView = null;
		recyclerView.removeOnScrollListener(this.onScrollListener);
	}

	/**
	 * Add the given {@code listener} to observe the recycler view scrolling state.
	 *
	 * @param listener the listener to be added.
	 * @throws NullPointerException if the given {@code listener} is null.
	 * @since 0.0.1 ~2021.04.08
	 */
	public void addOnScrollListener(OnScrollListener listener) {
		Objects.requireNonNull(listener, "listener");
		this.onScrollListeners.add(listener);
	}

	/**
	 * Return the items list of this adapter. Any changes to the list are observed by this
	 * adapter. The returned list has a synchronized access.
	 *
	 * @return the items list of this.
	 * @since 0.0.1 ~2021.04.09
	 */
	public List<Object> items() {
		return this.items;
	}

	/**
	 * Remove the given {@code listener} from observing the recycler view scrolling
	 * state.
	 *
	 * @param listener the listener to be removed.
	 * @throws NullPointerException if the given {@code listener} is null.
	 * @since 0.0.1 ~2021.04.08
	 */
	public void removeOnScrollListener(OnScrollListener listener) {
		Objects.requireNonNull(listener, "listener");
		this.onScrollListeners.remove(listener);
	}

	/**
	 * Set an auto view holder binder for the type {@code 0} with the given {@code
	 * layout}. No need to call {@link #setViewHolderSupplier(int, ViewHolderSupplier)}.
	 *
	 * @param layout the layout to be created for the given {@code type}.
	 * @param binder the binder.
	 * @param <T>    the type of the item.
	 * @throws NullPointerException if the given {@code binder} is null.
	 * @since 0.0.1 ~2021.04.09
	 */
	public <T> void setItemAutoViewHolderBinder(@LayoutRes int layout, ViewHolderBinder<AutoViewHolder, T> binder) {
		this.setItemAutoViewHolderBinder(0, layout, binder);
	}

	/**
	 * Set an auto view holder binder for the given {@code type} with the given {@code
	 * layout}. No need to call {@link #setViewHolderSupplier(int, ViewHolderSupplier)}.
	 *
	 * @param type   the type of items the {@code binder} can bind.
	 * @param layout the layout to be created for the given {@code type}.
	 * @param binder the binder.
	 * @param <T>    the type of the item.
	 * @throws NullPointerException if the given {@code binder} is null.
	 * @since 0.0.1 ~2021.04.09
	 */
	public <T> void setItemAutoViewHolderBinder(int type, @LayoutRes int layout, ViewHolderBinder<AutoViewHolder, T> binder) {
		this.setViewHolderSupplier(type, parent -> {
			View view = LayoutInflater.from(parent.getContext())
									  .inflate(layout, parent, false);
			return new AutoViewHolder(view);
		});
		this.setViewHolderBinder(type, binder);
	}

	/**
	 * Set the given {@code supplier} to be invoked when the adapter needs to know the
	 * type of a specific position.
	 *
	 * @param supplier the supplier to be set.
	 * @throws NullPointerException if the given {@code supplier} is null.
	 * @since 0.0.1 ~2021.04.06
	 */
	public void setItemViewTypeSupplier(@NotNull ItemViewTypeSupplier supplier) {
		Objects.requireNonNull(supplier, "supplier");
		this.itemViewTypeSupplier = supplier;
	}

	/**
	 * Set the given {@code binder} to be invoked when the adapter needs to bind an item
	 * to a view holder for the type {@code 0}.
	 *
	 * @param binder the binder to be invoked when in need to bind a view holder.
	 * @param <VH>   the type of the view holder.
	 * @param <T>    the type of the data of the item.
	 * @throws NullPointerException if the given {@code binder} is null.
	 * @since 0.0.1 ~2021.04.06
	 */
	public <VH extends RecyclerView.ViewHolder, T> void setViewHolderBinder(@NotNull InfiniteRecyclerViewAdapter.ViewHolderBinder<VH, T> binder) {
		this.setViewHolderBinder(0, binder);
	}

	/**
	 * Set the given {@code binder} to be invoked when the adapter needs to bind an item
	 * to a view holder with the given {@code type}.
	 *
	 * @param type   the type of items the {@code binder} can bind.
	 * @param binder the binder to be invoked when in need to bind a view holder.
	 * @param <VH>   the type of the view holder.
	 * @param <T>    the type of the data of the item.
	 * @throws NullPointerException if the given {@code binder} is null.
	 * @since 0.0.1 ~2021.04.06
	 */
	public <VH extends RecyclerView.ViewHolder, T> void setViewHolderBinder(int type, @NotNull InfiniteRecyclerViewAdapter.ViewHolderBinder<VH, T> binder) {
		Objects.requireNonNull(binder, "binder");
		this.viewHolderBinders.put(type, binder);
	}

	/**
	 * Set the given {@code supplier} to be invoked when the adapter needs to construct a
	 * new view holder for the type {@code 0}.
	 *
	 * @param supplier the supplier to be invoked.
	 * @param <VH>     the type of the view holder.
	 * @throws NullPointerException if the given {@code supplier} is null.
	 * @since 0.0.1 ~2021.04.06
	 */
	public <VH extends RecyclerView.ViewHolder> void setViewHolderSupplier(@NotNull InfiniteRecyclerViewAdapter.ViewHolderSupplier<VH> supplier) {
		this.setViewHolderSupplier(0, supplier);
	}

	/**
	 * Set the given {@code supplier} to be invoked when the adapter needs to construct a
	 * new view holder for the given {@code type}.
	 *
	 * @param type     the type of the view holder.
	 * @param supplier the supplier to be invoked.
	 * @param <VH>     the type of the view holder.
	 * @throws NullPointerException if the given {@code supplier} is null.
	 * @since 0.0.1 ~2021.04.06
	 */
	public <VH extends RecyclerView.ViewHolder> void setViewHolderSupplier(int type, @NotNull InfiniteRecyclerViewAdapter.ViewHolderSupplier<VH> supplier) {
		Objects.requireNonNull(supplier, "supplier");
		this.viewHolderSuppliers.put(type, supplier);
	}

	/**
	 * A listener to be invoked to know the type of an item position.
	 *
	 * @author LSafer
	 * @version 0.0.1
	 * @since 0.0.1 ~2021.04.06
	 */
	@FunctionalInterface
	public interface ItemViewTypeSupplier {
		/**
		 * Determine the type of the item at the given {@code position}.
		 *
		 * @param count    how many item are currently in the adapter.
		 * @param position the position of the item to get its type.
		 * @return the type of the item at the given {@code position}.
		 * @throws IllegalArgumentException if the given {@code count} or {@code position}
		 *                                  is negative.
		 * @since 0.0.1 ~2021.04.06
		 */
		int getItemViewType(
				@Range(from = 0, to = Integer.MAX_VALUE) int count,
				@Range(from = 0, to = Integer.MAX_VALUE) int position
		);
	}

	/**
	 * A listener to be invoked when a recycler view has a change in its scrolling state.
	 *
	 * @author LSafer
	 * @version 0.0.1
	 * @since 0.0.1 ~2021.04.08
	 */
	@FunctionalInterface
	public interface OnScrollListener {
		/**
		 * Invoked when the recycler view changed its scrolling state.
		 *
		 * @param count how many items in the recycler view.
		 * @param first the first visible item.
		 * @param last  the last visible item.
		 * @throws IllegalArgumentException if the given {@code count} or {@code first} or
		 *                                  {@code last} is negative.
		 * @since 0.0.1 ~2021.04.08
		 */
		void onScroll(
				@Range(from = 0, to = Integer.MAX_VALUE) int count,
				@Range(from = 0, to = Integer.MAX_VALUE) int first,
				@Range(from = 0, to = Integer.MAX_VALUE) int last
		);
	}

	/**
	 * A listener to be invoked to bind an item with a holder for a known type to the
	 * user.
	 *
	 * @param <VH> the type of the view holder.
	 * @param <T>  the type of the item.
	 * @author LSafer
	 * @version 0.0.1
	 * @since 0.0.1 ~2021.04.06
	 */
	@FunctionalInterface
	public interface ViewHolderBinder<VH extends RecyclerView.ViewHolder, T> {
		/**
		 * Binds the given {@code holder} with the given {@code item}.
		 *
		 * @param holder the holder to be bound.
		 * @param item   the item to bind the given {@code holder} with.
		 * @throws NullPointerException if the given {@code holder} is null.
		 * @since 0.0.1 ~2021.04.06
		 */
		void onBindViewHolder(@NotNull VH holder, @Nullable T item);
	}

	/**
	 * A listener to be invoked to create a new view-holder for a known type to the user.
	 *
	 * @param <VH> the view holder.
	 * @author LSafer
	 * @version 0.0.1
	 * @since 0.0.1 ~2021.04.06
	 */
	@FunctionalInterface
	public interface ViewHolderSupplier<VH extends RecyclerView.ViewHolder> {
		/**
		 * Construct a new view holder for the type this listener was registered for.
		 *
		 * @param parent the parent view to inflate the view with.
		 * @return the new view holder.
		 * @throws NullPointerException if the given {@code parent} is null.
		 * @since 0.0.1 ~2021.04.06
		 */
		@NotNull
		VH onCreateViewHolder(@NotNull ViewGroup parent);
	}

	/**
	 * An automatic view holder.
	 *
	 * @author LSafer
	 * @version 0.0.1
	 * @since 0.0.1 ~2021.04.09
	 */
	public static class AutoViewHolder extends RecyclerView.ViewHolder {
		/**
		 * Mappings for views previously found by {@link #findViewById(int)}.
		 *
		 * @since 0.0.1 ~2021.04.09
		 */
		protected final Map<Integer, View> map = new HashMap<>();

		/**
		 * Construct a new automatic view holder for the given {@code view}.
		 *
		 * @param view the view to be held.
		 * @throws NullPointerException if the given {@code view} is null.
		 * @since 0.0.1 ~2021.04.09
		 */
		public AutoViewHolder(@NonNull View view) {
			super(view);
		}

		/**
		 * Find the view that has the given {@code id} in the view held by this. Then,
		 * store that view to be returned when this method get invoked for the same id.
		 *
		 * @param id  the id of the view to be found.
		 * @param <V> the type of the view. (assumed)
		 * @return the found view. (or null if no such view was found)
		 * @since 0.0.1 ~2021.04.09
		 */
		public <V extends View> V findViewById(@IdRes int id) {
			return (V) this.map.computeIfAbsent(id, k ->
					this.itemView.findViewById(id)
			);
		}

		/**
		 * Return the view held by this.
		 *
		 * @return the view held by this.
		 * @since 0.0.1 ~2021.04.09
		 */
		@SuppressWarnings("SuspiciousGetterSetter")
		public View getView() {
			return this.itemView;
		}
	}
}
