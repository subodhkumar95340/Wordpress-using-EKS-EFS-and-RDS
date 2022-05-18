FROM wordpress:php7.1-apache

COPY ./wordpress /var/www/html/
RUN chown -R www-data. /var/www/html/
