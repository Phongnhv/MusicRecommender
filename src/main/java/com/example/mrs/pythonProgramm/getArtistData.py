import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from collections import defaultdict
import sys

client_id = 'CLIENT_ID'
client_secret = 'CLIENT_SECRET'
auth_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(auth_manager=auth_manager)

arg1 = sys.argv[1]
arg2 = sys.argv[2]

artist_id = ""

if arg2 == "id":
    artist_id = arg1
else:
    #number = int(arg2)
    res = sp.search(q=arg1, type='artist', limit = 1)
    artist_id = res['artists']['items'][0]['id']

# Get the artist's popular releases
releases = sp.artist_top_tracks(artist_id)
count = 0

related_artists = sp.artist_related_artists(artist_id)

for item in related_artists['artists']:
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

for track in releases['tracks']:
    print(track['album']['release_date'][:4])
    track['album']['release_date'][:4]
    print(track['name'])
    art = ""
    for artist in track['artists']:
        art = art + artist['name'] + ", "
    art = art[:-2]
    print(art)
    print(track['album']['images'][0]['url'])
    print(track['external_urls']['spotify'])

print("!")
# Get the artist's related artists
