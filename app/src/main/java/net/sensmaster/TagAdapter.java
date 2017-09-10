package net.sensmaster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class TagAdapter extends ArrayAdapter<Tag> {
  protected static final String TAG = "TagAdapter";
  private DatabaseHandler db;
  private Reader reader;
  private final Monitor context;

  public TagAdapter(Context context, int resource, int textViewResourceId, Reader _reader) {
    super(context, resource, textViewResourceId);
    reader = _reader;
    this.context = (Monitor) context;
    db = new DatabaseHandler(context, this);
  }

  public Tag rebuild(ByteArray uid) {
    for (int i = 0; i < size(); i++) {
      Tag tag = get(i);
      if(tag.toByteArray().isEqual(uid)) {
        return tag;
      }
    }
    Tag tag = new Tag(uid, context, reader, this);
    add(tag);
    return tag;
  }

  public Tag get(ByteArray uid) {
    for (int i = 0; i < size(); i++) {
      Tag tag = get(i);
      if(tag.toByteArray().isEqual(uid)) {
        return tag;
      }
    }
    Tag tag = new Tag(uid, context, reader, this);
    context.addText("Found: " + uid.toString());
    tag.pingServer();
    add(tag);
    db.addTag(tag);
    return tag;
  }

  private Tag get(int position) {
    return getItem(position);
  }

  private int size() {
    return getCount();
  }
  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    View rowView = convertView;
    final Tag tag = get(position);
    if (rowView == null) {
      LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      rowView = vi.inflate(R.layout.tag, parent, false);
    }

    TextView statusView = (TextView)rowView.findViewById(R.id.status);
    statusView.setText(tag.toString());

    ((Button) rowView.findViewById(R.id.getinfo)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        context.tagEvent("getting device info for " + tag, null);
        tag.CommandTag(Tag.GetDeviceInfo);
      }
    }); 

    ((Button) rowView.findViewById(R.id.makeRTF)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        tag.CommandTag(Tag.setRTFMode);
      }
    }); 

    ((Button) rowView.findViewById(R.id.getlog)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        context.tagEvent("getting log data from " + tag, null);
        tag.CommandTag(Tag.GetLog);
      }
    });

    ((Button) rowView.findViewById(R.id.settime)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        context.tagEvent("setting system time for " + tag, null);
        tag.CommandTag(Tag.SetSystemTime);
      }
    });

    ((Button) rowView.findViewById(R.id.setlog)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        context.tagEvent("setting device to start collecting data on " + tag, null);
        tag.CommandTag(Tag.SetLog);
      }
    });
    
    ((Button) rowView.findViewById(R.id.showlog)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        context.tagEvent("user looking at log data for " + tag, null);
        ListView list = (ListView) context.findViewById(R.id.in);
        tag.showLog(list);
      }
    });
    
    ((Button) rowView.findViewById(R.id.sendagain)).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        tag.sendDataToServer();
        context.tagEvent("uploading collected data for " + tag, null);
      }
    });

    return rowView;
  }

  public void update(Tag tag) {
    db.addTag(tag);
  }
}
