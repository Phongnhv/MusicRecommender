import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from collections import defaultdict
import sys

client_id = 'CLIENT_ID'
client_secret = 'CLIENT_SECRET'
auth_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(auth_manager=auth_manager)

# Set up Spotipy with your credentials
client_credentials_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(client_credentials_manager=client_credentials_manager)

querry = ""
querry = sys.argv[1]

# Perform a search on Spotify
result = sp.search(q=querry, type='artist', limit = 5)

art_name = ""
track_name = ""

art_name = result['artists']['items'][0]['name']

# Iterate over the search results
for item in result['artists']['items']:
    artist_name = item['name']
    print(artist_name)
    cover_url = item['images'][0]['url'] if item['images'] else None
    if  cover_url == None: print("None")
    else : print(cover_url)
    print(item['id'])
    spotify_url = item['external_urls']['spotify']
    if  spotify_url == None: print("None")
    else : print(spotify_url)
    follower = item['followers']['total']
    print(follower)

print("!")
result = sp.search(q = querry, type = "track,artist", limit = 5)
track_name = result['tracks']['items'][0]['name']

for item in result['tracks']['items']:
    track = item
    song_data = defaultdict(str)
    song_data['year'] = int(track['album']['release_date'][:4])
    song_data['name'] = track['name']
    artists = ", ".join(artist['name'] for artist in track['artists'])
    song_data['artist'] = artists
    song_data['cover_url'] = track['album']['images'][0]['url']
    song_data['external_url'] = track['external_urls']['spotify']

    print(song_data['year'])
    print(song_data['name'])
    print(song_data['artist'])
    print(song_data['cover_url'])
    print(song_data['external_url'])

print("!")
from difflib import SequenceMatcher

def choose_string(name, option1, option2):
    similarity1 = SequenceMatcher(None, name, option1).ratio()
    similarity2 = SequenceMatcher(None, name, option2).ratio()

    if similarity1 >= similarity2:
        print("artist")
        return option1
    else:
        print("tracks")
        return option2


choose_string(querry, art_name, track_name)
