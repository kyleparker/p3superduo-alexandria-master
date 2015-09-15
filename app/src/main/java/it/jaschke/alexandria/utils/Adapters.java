package it.jaschke.alexandria.utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import it.jaschke.alexandria.R;
import it.jaschke.alexandria.objects.Book;

/**
 * Adapters for RecyclerView
 *
 * Created by kyleparker on 9/14/2015.
 */
public class Adapters {
    /**
     * Guide adapter to populate the list of available guides
     */
    public static class BookListAdapter extends RecyclerView.Adapter<BookListAdapter.ViewHolder> {
        private Context mContext;
        private List<Book> mItems;
        private OnItemClickListener mOnItemClickListener;

        public BookListAdapter(Context context) {
            mContext = context;
            mItems = new ArrayList<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_book, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            Book item = mItems.get(position);

            if (item != null) {
                if (!TextUtils.isEmpty(item.getImageUrl())) {
                    // DONE: This needs to be swapped out for Picasso
                    Picasso.with(mContext)
                            .load(item.getImageUrl())
                            .resize(300, 300)
                            .centerCrop()
                            .placeholder(R.drawable.ic_placeholder_book)
                            .into(viewHolder.bookCover);
                } else {
                    viewHolder.bookCover.setImageResource(R.drawable.ic_placeholder_book);
                }

                viewHolder.bookTitle.setText(item.getTitle());

                if (!TextUtils.isEmpty(item.getSubtitle())) {
                    viewHolder.bookSubtitle.setText(item.getSubtitle());
                    viewHolder.bookSubtitle.setVisibility(View.VISIBLE);
                }

                if (!TextUtils.isEmpty(item.author.getName())) {
                    String[] authorsArr = item.author.getName().split(",");
                    viewHolder.bookAuthor.setLines(authorsArr.length);
                    viewHolder.bookAuthor.setText(item.author.getName().replace(",", "\n"));
                    viewHolder.bookAuthor.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public Book getItem(int position) {
            return mItems.get(position);
        }

        public void addAll(List<Book> books) {
            mItems.clear();
            mItems.addAll(books);

            notifyDataSetChanged();
        }

        public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
            mOnItemClickListener = itemClickListener;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public final BezelImageView bookCover;
            public final TextView bookTitle;
            public final TextView bookSubtitle;
            public final TextView bookAuthor;

            public ViewHolder(View base) {
                super(base);

                bookCover = (BezelImageView) itemView.findViewById(R.id.book_cover);
                bookTitle = (TextView) itemView.findViewById(R.id.book_title);
                bookSubtitle = (TextView) itemView.findViewById(R.id.book_subtitle);
                bookAuthor = (TextView) itemView.findViewById(R.id.book_author);

                base.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, getAdapterPosition());
                }
            }
        }

        public interface OnItemClickListener {
            void onItemClick(View view, int position);
        }
    }
}
