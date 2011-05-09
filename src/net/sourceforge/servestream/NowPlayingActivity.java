/*
 * ServeStream: A HTTP stream browser/player for Android
 * Copyright 2010 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sourceforge.servestream;

import net.sourceforge.servestream.service.IMediaService;
import net.sourceforge.servestream.service.MediaService;
import net.sourceforge.servestream.utils.MediaFile;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class NowPlayingActivity extends ListActivity {
	public final static String TAG = "ServeStream.NowPlayingActivity";
	
    private IMediaService mMediaService = null;
	
	protected LayoutInflater mInflater = null;
	
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaService.META_CHANGED)) {
                updateList();
            } else if (action.equals(MediaService.PLAYSTATE_CHANGED)) {
                //setPauseButtonImage();
            }
        }
    };
	
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  Because we have bound to a explicit
	        // service that we know is running in our own process, we can
	        // cast its IBinder to a concrete class and directly access it.
            mMediaService = IMediaService.Stub.asInterface(obj);
            
        	updateList();
        }
        public void onServiceDisconnected(ComponentName classname) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        // Because it is running in our same process, we should never
	        // see this happen.
            mMediaService = null;
        }
    };
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.act_nowplaying);

		this.setTitle(String.format("%s: %s",
				getResources().getText(R.string.app_name),
				getResources().getText(R.string.title_now_playing)));
		
		ListView list = this.getListView();
		list.setFastScrollEnabled(true);

		list.setOnItemClickListener(new OnItemClickListener() {
			public synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//mCurrentURL = mCurrentListing.get(position);
				Log.v(TAG, "Pressed");
				try {
					mMediaService.setQueuePosition(position);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		
		this.mInflater = LayoutInflater.from(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// connect with manager service to find all bridges
		// when connected it will insert all views
		bindService(new Intent(this, MediaService.class), connection, Context.BIND_AUTO_CREATE);
		
        IntentFilter f = new IntentFilter();
        f.addAction(MediaService.PLAYSTATE_CHANGED);
        f.addAction(MediaService.META_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(f));
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
        // Detach our existing connection.
        unbindService(connection);
        mMediaService = null;
        
        unregisterReceiver(mStatusListener);
	}
	
	protected void updateList() {
		
		MediaFile[] streams = null;
		
		try {
			streams = mMediaService.getQueue();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		NowPlayingAdapter adapter = new NowPlayingAdapter(this, streams);

		this.setListAdapter(adapter);
	}
	
	class NowPlayingAdapter extends ArrayAdapter<MediaFile> {
		
		private MediaFile [] streams;

		class ViewHolder {
			public TextView trackNumber;
			public ImageView icon;
			public TextView trackTitle;
			public TextView trackURL;
		}

		public NowPlayingAdapter(Context context, MediaFile [] streams) {
			super(context, R.layout.item_nowplaying, streams);

			this.streams = streams;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.item_nowplaying, null, false);

				holder = new ViewHolder();

				holder.trackNumber = (TextView)convertView.findViewById(R.id.number);
				holder.trackTitle = (TextView)convertView.findViewById(R.id.title);
				holder.trackURL = (TextView)convertView.findViewById(R.id.url);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				
				convertView.setTag(holder);
			} else
				holder = (ViewHolder) convertView.getTag();

			MediaFile stream = streams[position];
			
			holder.trackNumber.setText(String.valueOf(stream.getTrackNumber()));
			holder.trackTitle.setText(String.valueOf(stream.getTitle()));
			holder.trackURL.setText(String.valueOf(stream.getURL()));

			try {
				if (mMediaService.getQueuePosition() == position) {
					holder.icon.setBackgroundResource(R.drawable.volume);
				} else {
					holder.icon.setBackgroundResource(R.drawable.none);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			return convertView;
		}
	}
}
