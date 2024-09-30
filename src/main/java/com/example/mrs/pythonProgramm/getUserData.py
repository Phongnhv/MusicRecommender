import spotipy
from spotipy.oauth2 import SpotifyOAuth
import mysql.connector

# Set up Spotify API credentials
client_id = 'CLIENT_ID'
client_secret = 'CLIENT_SECRET'
redirect_uri = "http://localhost:3000"

# Create a Spotify OAuth object
scope = "user-read-recently-played user-top-read user-read-email"
sp = spotipy.Spotify(auth_manager=SpotifyOAuth(client_id=client_id,
                                               client_secret=client_secret,
                                               redirect_uri=redirect_uri,
                                               scope=scope, show_dialog = True))

user_profile = sp.current_user()
user_email = user_profile['email']
print(user_email)


connection = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="mr_sys"
)
cursor = connection.cursor()

sql = "SELECT userID FROM user_data WHERE spotify_email = '" + user_email + "'"
cursor.execute(sql)
results = cursor.fetchall()
userID = [row[0] for row in results]
userid = userID[0]

if not results:
    with open("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt", "w") as file:
        file.write('0'+ '\n' + str(userid))
else:
    with open("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt", "w") as file:
        file.write('1'+ '\n' + str(userid))
file.close()

sql = "DELETE FROM user_recently_played WHERE user = " + str(userid)
cursor.execute(sql)
connection.commit()

unique_tracks = set()

# Get the user's recently played tracks
recently_played = sp.current_user_recently_played()
for track in recently_played["items"]:
    track_info = track["track"]
    track_id = track_info["id"]
    if track_id in unique_tracks:
        continue  # Skip duplicate track

    # Add the track ID to the set
    unique_tracks.add(track_id)
    print(track_info['name'])
    artists = ""
    for artist in track_info['artists']:
        artists = artists + artist['name'] + ", "
    artists = artists[:-2]
    print(artists)
    year = track_info['album']['release_date'][:4]
    cover_url = track_info['album']['images'][0]['url']
    d = track_info['external_urls']
    external_url = d['spotify']
    sql = "INSERT INTO user_recently_played (name, artists, user,year,cover_url, external_url) VALUES (%s, %s ,%s, %s, %s, %s)"
    values = (track_info['name'], artists, userid, str(year), cover_url, external_url)
    cursor.execute(sql, values)
    connection.commit()

# Get the user's top played tracks
#top_played = sp.current_user_top_tracks(limit=10)
    #for track in top_played["items"]:
    #track_info = track.get("track")
    #if track_info is None:
        #print("Missing 'track' key in the track dictionary")
    #    continue
    #artists = ""
    #for artist in track.get('artists'):
    #    artists = artists + artist['name'] + ", "
    #sql = "INSERT INTO top_player (name, artists, user) VALUES (%s, %s ,%s)"
    #values = (track_info['name'], artists, userid)
    #cursor.execute(sql, values)
#connection.commit()
