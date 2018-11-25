package de.christeck.notenanzeiger;

import android.support.v7.widget.RecyclerView;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.view.LayoutInflater;


public class ThumbnailRecyclerViewAdapter extends RecyclerView.Adapter<ThumbnailRecyclerViewAdapter.ViewHolder> {

	private PdfPageRenderer pageRenderer;
    private LayoutInflater layoutInflater;
    private ItemClickListener clickListener;

    // The second arguments is the actual data source, maybe a string array or a renderer
    ThumbnailRecyclerViewAdapter(Context context, PdfPageRenderer renderer) {
        this.layoutInflater = LayoutInflater.from(context);
		this.pageRenderer = renderer;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data source to the dynamically created views
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
		holder.thumbnailView.setImageBitmap(pageRenderer.renderPage(position, true));
    }


    @Override
    public int getItemCount() {
        return pageRenderer.pageCount();
    }


    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView thumbnailView;

        ViewHolder(View itemView) {
            super(itemView);
			thumbnailView = itemView.findViewById(R.id.thumbnailView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) clickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}