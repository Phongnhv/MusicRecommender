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
                                               scope=scope, show_dialog= True))

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
num = 1
if not results:
    sql = "INSERT INTO user_data (username, spotify_email) VALUES (%s, %s)"
    values = (user_email, user_email)
    cursor.execute(sql,values)
    connection.commit()

else:
    num = 0

with open("D:\\IdeaProjects\\MRS\\src\\main\\java\\com\\example\\mrs\\tmp\\tmpValue.txt", "w") as file:
    file.write(str(num))
file.close()
