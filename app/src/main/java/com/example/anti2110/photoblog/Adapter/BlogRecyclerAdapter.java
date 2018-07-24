package com.example.anti2110.photoblog.Adapter;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.anti2110.photoblog.Model.BlogPost;
import com.example.anti2110.photoblog.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blogList;
    public Context context;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public BlogRecyclerAdapter(List<BlogPost> blogList) {
        this.blogList = blogList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item, parent, false);
        context = parent.getContext();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.setIsRecyclable(false);

        final String blogPostId = blogList.get(position).BlogPostId;
        final String currentUserId = auth.getCurrentUser().getUid();

        String desc_data = blogList.get(position).getDesc();
        holder.setDescText(desc_data);

        String image_url = blogList.get(position).getImage_url();
        holder.setBlogImage(image_url);

        String user_id = blogList.get(position).getUser_id();

        firestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    String username = task.getResult().getString("name");
                    String userImage = task.getResult().getString("image");

                    holder.setUserData(username, userImage);
                } else {

                }
            }
        });

        long milliseconds = blogList.get(position).getTimestamp().getTime();
        String dateString = DateFormat.format("MM/dd/yyyy", new Date(milliseconds)).toString();
        holder.setTime(dateString);

        firestore.collection("Posts")
                .document(blogPostId)
                .collection("Likes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        if(!documentSnapshots.isEmpty()) {
                            int count = documentSnapshots.size();
                            holder.updateLikeCount(count);
                        } else {
                            holder.updateLikeCount(0);
                        }
                    }
                });

        firestore.collection("Posts")
                .document(blogPostId)
                .collection("Likes")
                .document(currentUserId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                        if(documentSnapshot.exists()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.drawable.ic_favorite_red));
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.drawable.ic_favorite));
                            }
                        }
                    }
                });

        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                firestore.collection("Posts")
                        .document(blogPostId)
                        .collection("Likes")
                        .document(currentUserId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(!task.getResult().exists()) {
                                    Map<String, Object> likesMap = new HashMap<>();
                                    likesMap.put("timestamp", FieldValue.serverTimestamp());

                                    // firestore.collection("Posts/" + blogPostId + "/Likes")
                                    firestore.collection("Posts")
                                            .document(blogPostId)
                                            .collection("Likes")
                                            .document(currentUserId)
                                            .set(likesMap);
                                } else {
                                    firestore.collection("Posts")
                                            .document(blogPostId)
                                            .collection("Likes")
                                            .document(currentUserId)
                                            .delete();
                                }
                            }
                        });
            }
        });

    }

    @Override
    public int getItemCount() {
        return blogList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private TextView descView;
        private ImageView blogImageView;
        private TextView username;
        private TextView blogDate;
        private CircleImageView profileImage;

        private ImageView blogLikeBtn;
        private TextView blogLikeCount;

        public ViewHolder(View itemView) {
            super(itemView);
            mView = itemView;

            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
        }

        public void setDescText(String descText) {
            descView = mView.findViewById(R.id.blog_description);
            descView.setText(descText);
        }

        public void setBlogImage(String downloadUri) {
            blogImageView = mView.findViewById(R.id.blog_list_image);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.avatar);
            Glide.with(context)
                    .applyDefaultRequestOptions(requestOptions)
                    .load(downloadUri)
                    .into(blogImageView);
        }

        public void setTime(String date) {
            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);
        }

        public void setUserData(String name, String image){
            username = mView.findViewById(R.id.blog_username);
            profileImage = mView.findViewById(R.id.blog_list_profile_image);
            username.setText(name);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.avatar);
            Glide.with(context)
                    .applyDefaultRequestOptions(requestOptions)
                    .load(image)
                    .into(profileImage);
        }

        public void updateLikeCount(int count) {
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
            blogLikeCount.setText(count+" LIkes");
        }

    }

}
