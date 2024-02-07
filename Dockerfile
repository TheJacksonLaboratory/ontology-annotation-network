# Set up running image
FROM neo4j:community-bullseye

COPY ./neo4j/data/ /data
RUN neo4j-admin dbms set-initial-password password
EXPOSE 7474 7687

CMD ["neo4j", "start", "-d neo4j"]


