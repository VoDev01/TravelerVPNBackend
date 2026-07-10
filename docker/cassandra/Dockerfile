FROM cassandra

COPY ./docker/cassandra/entrypoint-wrap.sh /usr/local/bin/entrypoint-wrap.sh
RUN chmod +x /usr/local/bin/entrypoint-wrap.sh
ENTRYPOINT ["entrypoint-wrap.sh"]
CMD ["cassandra", "-f"]